namespace DMSbackend.Options;

public sealed class GroqOptions
{
    public string ApiKey { get; set; } = string.Empty;
    public string Model { get; set; } = "openai/gpt-oss-20b";
}

public sealed class BrevoOptions
{
    public string ApiKey { get; set; } = string.Empty;
    public string SenderEmail { get; set; } = "drivemonitorsystem@gmail.com";
}

public sealed class StorageOptions
{
    public string RootPath { get; set; } = "App_Data/Images";
    public int RetentionHours { get; set; } = 72;
    public long MaxImageBytes { get; set; } = 10 * 1024 * 1024;
}
