// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Vudinhdat02

namespace DMSbackend.Data;

public sealed class StoredImage
{
    public Guid Id { get; set; }
    public required string UserId { get; set; }
    public required string FilePath { get; set; }
    public required string ContentType { get; set; }
    public long SizeBytes { get; set; }
    public DateTime CreatedAtUtc { get; set; }
}
