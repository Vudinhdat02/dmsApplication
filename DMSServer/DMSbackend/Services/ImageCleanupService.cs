using DMSbackend.Data;
using DMSbackend.Options;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Options;

namespace DMSbackend.Services;

public sealed class ImageCleanupService(
    IServiceScopeFactory scopeFactory,
    IOptions<StorageOptions> options,
    ILogger<ImageCleanupService> logger) : BackgroundService
{
    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        await CleanupAsync(stoppingToken);
        using var timer = new PeriodicTimer(TimeSpan.FromHours(1));
        while (await timer.WaitForNextTickAsync(stoppingToken))
            await CleanupAsync(stoppingToken);
    }

    private async Task CleanupAsync(CancellationToken cancellationToken)
    {
        try
        {
            await using var scope = scopeFactory.CreateAsyncScope();
            var db = scope.ServiceProvider.GetRequiredService<DmsDbContext>();
            var cutoff = DateTime.UtcNow.AddHours(-Math.Max(1, options.Value.RetentionHours));
            var expired = await db.Images.Where(item => item.CreatedAtUtc < cutoff)
                .ToListAsync(cancellationToken);
            foreach (var image in expired)
            {
                try
                {
                    if (File.Exists(image.FilePath)) File.Delete(image.FilePath);
                    db.Images.Remove(image);
                }
                catch (IOException exception)
                {
                    logger.LogWarning(exception, "Không thể xóa ảnh hết hạn {ImageId}", image.Id);
                }
            }
            if (expired.Count > 0) await db.SaveChangesAsync(cancellationToken);
        }
        catch (OperationCanceledException) when (cancellationToken.IsCancellationRequested)
        {
        }
        catch (Exception exception)
        {
            logger.LogError(exception, "Dọn ảnh hết hạn thất bại");
        }
    }
}
