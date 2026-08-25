using System.Security.Claims;
using DMSbackend.Data;
using DMSbackend.Models;
using DMSbackend.Options;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.RateLimiting;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Options;

namespace DMSbackend.Controllers;

[ApiController]
[Authorize]
[Route("api/images")]
public sealed class ImagesController(
    DmsDbContext db,
    IOptions<StorageOptions> options,
    IWebHostEnvironment environment) : ControllerBase
{
    [HttpPost("upload")]
    [EnableRateLimiting("uploads")]
    [RequestSizeLimit(10 * 1024 * 1024)]
    public async Task<ActionResult<ImageUploadResponse>> Upload(
        IFormFile file,
        CancellationToken cancellationToken)
    {
        var uid = CurrentUid();
        if (file.Length <= 0 || file.Length > options.Value.MaxImageBytes)
            return StatusCode(413, new { message = "Ảnh rỗng hoặc vượt quá 10 MB" });

        await using var input = file.OpenReadStream();
        var header = new byte[12];
        var bytesRead = await input.ReadAsync(header, cancellationToken);
        input.Position = 0;
        var imageType = DetectImageType(header.AsSpan(0, bytesRead));
        if (imageType is null) return StatusCode(415, new { message = "Chỉ hỗ trợ JPEG, PNG hoặc WebP" });

        var id = Guid.NewGuid();
        var root = Path.GetFullPath(Path.Combine(environment.ContentRootPath, options.Value.RootPath));
        var userFolder = Path.Combine(root, uid);
        Directory.CreateDirectory(userFolder);
        var filePath = Path.Combine(userFolder, $"{id:N}{imageType.Value.Extension}");

        try
        {
            await using (var output = new FileStream(
                filePath, FileMode.CreateNew, FileAccess.Write, FileShare.None, 81920, true))
            {
                await input.CopyToAsync(output, cancellationToken);
            }
            var now = DateTime.UtcNow;
            db.Images.Add(new StoredImage
            {
                Id = id,
                UserId = uid,
                FilePath = filePath,
                ContentType = imageType.Value.ContentType,
                SizeBytes = file.Length,
                CreatedAtUtc = now
            });
            await db.SaveChangesAsync(cancellationToken);
            return Ok(new ImageUploadResponse(
                id,
                $"/api/images/{id}",
                now,
                now.AddHours(options.Value.RetentionHours)));
        }
        catch
        {
            if (System.IO.File.Exists(filePath)) System.IO.File.Delete(filePath);
            throw;
        }
    }

    [HttpGet]
    public async Task<ActionResult<IReadOnlyList<ImageListItem>>> List(CancellationToken cancellationToken)
    {
        var uid = CurrentUid();
        var retention = options.Value.RetentionHours;
        var items = await db.Images.AsNoTracking()
            .Where(item => item.UserId == uid)
            .OrderByDescending(item => item.CreatedAtUtc)
            .Select(item => new ImageListItem(
                item.Id,
                $"/api/images/{item.Id}",
                item.ContentType,
                item.SizeBytes,
                item.CreatedAtUtc,
                item.CreatedAtUtc.AddHours(retention)))
            .ToListAsync(cancellationToken);
        return Ok(items);
    }

    [HttpGet("{id:guid}")]
    public async Task<IActionResult> Download(Guid id, CancellationToken cancellationToken)
    {
        var uid = CurrentUid();
        var image = await db.Images.AsNoTracking()
            .SingleOrDefaultAsync(item => item.Id == id && item.UserId == uid, cancellationToken);
        if (image is null || !System.IO.File.Exists(image.FilePath)) return NotFound();
        Response.Headers.CacheControl = "private, max-age=300";
        return PhysicalFile(image.FilePath, image.ContentType, enableRangeProcessing: true);
    }

    [HttpDelete("{id:guid}")]
    public async Task<ActionResult<OperationResponse>> Delete(Guid id, CancellationToken cancellationToken)
    {
        var uid = CurrentUid();
        var image = await db.Images
            .SingleOrDefaultAsync(item => item.Id == id && item.UserId == uid, cancellationToken);
        if (image is null) return Ok(new OperationResponse(true));
        if (System.IO.File.Exists(image.FilePath)) System.IO.File.Delete(image.FilePath);
        db.Images.Remove(image);
        await db.SaveChangesAsync(cancellationToken);
        return Ok(new OperationResponse(true));
    }

    private string CurrentUid() =>
        User.FindFirstValue("user_id")
        ?? User.FindFirstValue("sub")
        ?? throw new UnauthorizedAccessException();

    private static (string ContentType, string Extension)? DetectImageType(ReadOnlySpan<byte> bytes)
    {
        if (bytes.Length >= 3 && bytes[0] == 0xFF && bytes[1] == 0xD8 && bytes[2] == 0xFF)
            return ("image/jpeg", ".jpg");
        if (bytes.Length >= 8 && bytes[..8].SequenceEqual(new byte[] { 137, 80, 78, 71, 13, 10, 26, 10 }))
            return ("image/png", ".png");
        if (bytes.Length >= 12
            && bytes[..4].SequenceEqual("RIFF"u8)
            && bytes.Slice(8, 4).SequenceEqual("WEBP"u8))
            return ("image/webp", ".webp");
        return null;
    }
}
