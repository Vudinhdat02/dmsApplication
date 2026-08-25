using System.Net.Http.Headers;
using System.Net.Mail;
using System.Text.Json;

namespace DMSbackend.Services;

public sealed class FirestoreContactsService(HttpClient httpClient, IConfiguration configuration)
{
    public async Task<IReadOnlyList<string>> GetContactsAsync(
        string userId,
        string firebaseIdToken,
        CancellationToken cancellationToken)
    {
        var projectId = configuration["Firebase:ProjectId"]
            ?? throw new InvalidOperationException("Thiếu Firebase:ProjectId");
        var encodedUid = Uri.EscapeDataString(userId);
        var url = $"v1/projects/{projectId}/databases/(default)/documents/users/{encodedUid}";
        using var request = new HttpRequestMessage(HttpMethod.Get, url);
        request.Headers.Authorization = new AuthenticationHeaderValue("Bearer", firebaseIdToken);
        using var response = await httpClient.SendAsync(request, cancellationToken);
        if (!response.IsSuccessStatusCode)
            throw new HttpRequestException($"Không thể đọc liên hệ khẩn cấp: HTTP {(int)response.StatusCode}");

        await using var stream = await response.Content.ReadAsStreamAsync(cancellationToken);
        using var json = await JsonDocument.ParseAsync(stream, cancellationToken: cancellationToken);
        if (!json.RootElement.TryGetProperty("fields", out var fields)
            || !fields.TryGetProperty("emergencyEmails", out var emails)
            || !emails.TryGetProperty("arrayValue", out var arrayValue)
            || !arrayValue.TryGetProperty("values", out var values))
            return Array.Empty<string>();

        var result = new List<string>(2);
        foreach (var value in values.EnumerateArray())
        {
            if (!value.TryGetProperty("stringValue", out var stringValue)) continue;
            var email = stringValue.GetString();
            if (string.IsNullOrWhiteSpace(email)) continue;
            try
            {
                _ = new MailAddress(email);
                result.Add(email);
            }
            catch (FormatException)
            {
                // Bỏ qua email không hợp lệ trong dữ liệu người dùng.
            }
            if (result.Count == 2) break;
        }
        return result;
    }
}
