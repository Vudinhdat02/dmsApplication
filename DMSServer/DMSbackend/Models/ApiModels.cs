using System.ComponentModel.DataAnnotations;

namespace DMSbackend.Models;

public sealed record DrivingAnalysisRequest(
    [Range(0, 100)] int DailyScore,
    [Range(0, 10000)] int TodayErrors,
    [Range(0, 10000)] int Morning,
    [Range(0, 10000)] int Afternoon,
    [Range(0, 10000)] int Evening,
    [Range(0, 10000)] int Night,
    [Range(0, 10000)] int HighSpeed);

public sealed record CrashAlertRequest(
    [Range(-90, 90)] double Latitude,
    [Range(-180, 180)] double Longitude);

public sealed record TextResponse(string Text);
public sealed record OperationResponse(bool Success);
public sealed record ImageUploadResponse(
    Guid Id,
    string Path,
    DateTime CreatedAtUtc,
    DateTime ExpiresAtUtc);
public sealed record ImageListItem(
    Guid Id,
    string Path,
    string ContentType,
    long SizeBytes,
    DateTime CreatedAtUtc,
    DateTime ExpiresAtUtc);
