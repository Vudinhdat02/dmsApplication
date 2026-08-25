using System.Net.Http.Headers;
using System.Text;
using System.Text.Json;
using DMSbackend.Models;
using DMSbackend.Options;
using Microsoft.Extensions.Options;

namespace DMSbackend.Services;

public sealed class GroqService(HttpClient httpClient, IOptions<GroqOptions> options)
{
    public async Task<string> AnalyzeAsync(DrivingAnalysisRequest data, CancellationToken cancellationToken)
    {
        var config = options.Value;
        if (string.IsNullOrWhiteSpace(config.ApiKey))
            throw new InvalidOperationException("Server chưa cấu hình Groq:ApiKey");

        var prompt = $"""
            Điểm an toàn hôm nay: {data.DailyScore}/100.
            Tổng số lần mất tập trung: {data.TodayErrors}.
            Mất tập trung ở tốc độ cao trên 60 km/h: {data.HighSpeed}.
            Phân bố theo khung giờ: sáng {data.Morning}, chiều {data.Afternoon}, tối {data.Evening}, đêm {data.Night}.
            Hãy chỉ ra khung giờ tài xế hay mệt nhất, cảnh báo về tốc độ nếu có và đưa ra một lời khuyên thực tế.
            """;
        var payload = new
        {
            model = config.Model,
            messages = new object[]
            {
                new
                {
                    role = "system",
                    content = "Bạn là trợ lý AI phân tích an toàn lái xe. Trả lời cực kỳ ngắn gọn khoảng 40 từ, thân thiện, dùng tiếng Việt, không chào hỏi."
                },
                new { role = "user", content = prompt }
            }
        };

        using var request = new HttpRequestMessage(HttpMethod.Post, "openai/v1/chat/completions");
        request.Headers.Authorization = new AuthenticationHeaderValue("Bearer", config.ApiKey);
        request.Content = new StringContent(JsonSerializer.Serialize(payload), Encoding.UTF8, "application/json");
        using var response = await httpClient.SendAsync(request, cancellationToken);
        if (!response.IsSuccessStatusCode)
            throw new HttpRequestException($"Groq trả về HTTP {(int)response.StatusCode}");

        await using var stream = await response.Content.ReadAsStreamAsync(cancellationToken);
        using var json = await JsonDocument.ParseAsync(stream, cancellationToken: cancellationToken);
        var text = json.RootElement.GetProperty("choices")[0]
            .GetProperty("message").GetProperty("content").GetString()?.Trim();
        if (string.IsNullOrWhiteSpace(text))
            throw new HttpRequestException("Groq không trả về nội dung");
        return text.Replace("*", string.Empty);
    }
}
