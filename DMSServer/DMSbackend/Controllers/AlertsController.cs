using System.Collections.Concurrent;
using System.Security.Claims;
using DMSbackend.Models;
using DMSbackend.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.RateLimiting;

namespace DMSbackend.Controllers;

[ApiController]
[Authorize]
[Route("api/send-crash-alert")]
public sealed class AlertsController(
    FirestoreContactsService contactsService,
    BrevoService brevo,
    ILogger<AlertsController> logger) : ControllerBase
{
    private static readonly ConcurrentDictionary<string, DateTime> LastAlertByUser = new();

    [HttpPost]
    [EnableRateLimiting("alerts")]
    public async Task<ActionResult<OperationResponse>> Send(
        [FromBody] CrashAlertRequest request,
        CancellationToken cancellationToken)
    {
        var uid = User.FindFirstValue("user_id") ?? User.FindFirstValue("sub");
        var authorization = Request.Headers.Authorization.ToString();
        var idToken = authorization.StartsWith("Bearer ", StringComparison.OrdinalIgnoreCase)
            ? authorization[7..].Trim()
            : string.Empty;
        if (string.IsNullOrWhiteSpace(uid) || string.IsNullOrWhiteSpace(idToken)) return Unauthorized();

        var now = DateTime.UtcNow;
        if (LastAlertByUser.TryGetValue(uid, out var previous)
            && now - previous < TimeSpan.FromSeconds(30))
            return StatusCode(429, new { message = "Cảnh báo đang trong thời gian chờ" });

        try
        {
            var contacts = await contactsService.GetContactsAsync(uid, idToken, cancellationToken);
            if (contacts.Count == 0)
                return BadRequest(new { message = "Tài khoản chưa có email liên hệ khẩn cấp" });
            await brevo.SendCrashAlertAsync(
                contacts, request.Latitude, request.Longitude, cancellationToken);
            LastAlertByUser[uid] = now;
            return Ok(new OperationResponse(true));
        }
        catch (InvalidOperationException exception)
        {
            logger.LogError(exception, "Brevo chưa được cấu hình");
            return Problem("Dịch vụ email chưa được cấu hình", statusCode: 503);
        }
        catch (HttpRequestException exception)
        {
            logger.LogWarning(exception, "Gửi cảnh báo thất bại cho UID {Uid}", uid);
            return Problem("Không thể gửi cảnh báo", statusCode: 502);
        }
    }
}
