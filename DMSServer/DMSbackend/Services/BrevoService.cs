using System.Net.Http.Headers;
using System.Text;
using System.Text.Encodings.Web;
using System.Text.Json;
using DMSbackend.Options;
using Microsoft.Extensions.Options;

namespace DMSbackend.Services;

public sealed class BrevoService(HttpClient httpClient, IOptions<BrevoOptions> options)
{
    public async Task SendCrashAlertAsync(
        IReadOnlyCollection<string> recipients,
        double latitude,
        double longitude,
        CancellationToken cancellationToken)
    {
        var config = options.Value;
        if (string.IsNullOrWhiteSpace(config.ApiKey))
            throw new InvalidOperationException("Server chưa cấu hình Brevo:ApiKey");
        if (string.IsNullOrWhiteSpace(config.SenderEmail))
            throw new InvalidOperationException("Server chưa cấu hình Brevo:SenderEmail");

        var location = $"https://www.google.com/maps/search/?api=1&query={latitude},{longitude}";
        var payload = new
        {
            sender = new
            {
                email = config.SenderEmail,
                name = "DMS - Hệ thống giám sát trạng thái người lái"
            },
            to = recipients.Select(email => new { email }).ToArray(),
            subject = "CẢNH BÁO KHẨN CẤP: PHÁT HIỆN VA CHẠM",
            htmlContent = $"<h3>PHÁT HIỆN TAI NẠN!</h3><p>Hệ thống DMS ghi nhận xe vừa xảy ra va chạm mạnh.</p><p>Vị trí hiện tại: <a href='{HtmlEncoder.Default.Encode(location)}'>Mở Google Maps</a></p>"
        };

        using var request = new HttpRequestMessage(HttpMethod.Post, "v3/smtp/email");
        request.Headers.Add("api-key", config.ApiKey);
        request.Headers.Accept.Add(new MediaTypeWithQualityHeaderValue("application/json"));
        request.Content = new StringContent(JsonSerializer.Serialize(payload), Encoding.UTF8, "application/json");
        using var response = await httpClient.SendAsync(request, cancellationToken);
        if (!response.IsSuccessStatusCode)
            throw new HttpRequestException($"Brevo trả về HTTP {(int)response.StatusCode}");
    }
}
