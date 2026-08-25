using Microsoft.EntityFrameworkCore;

namespace DMSbackend.Data;

public sealed class DmsDbContext(DbContextOptions<DmsDbContext> options) : DbContext(options)
{
    public DbSet<StoredImage> Images => Set<StoredImage>();

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        var image = modelBuilder.Entity<StoredImage>();
        image.HasKey(item => item.Id);
        image.Property(item => item.UserId).HasMaxLength(128).IsRequired();
        image.Property(item => item.FilePath).HasMaxLength(512).IsRequired();
        image.Property(item => item.ContentType).HasMaxLength(64).IsRequired();
        image.HasIndex(item => new { item.UserId, item.CreatedAtUtc });
        image.HasIndex(item => item.CreatedAtUtc);
    }
}
