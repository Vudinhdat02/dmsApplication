using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace DMSbackend.Controllers;

[ApiController]
[Route("health")]
public sealed class HealthController : ControllerBase
{
    [AllowAnonymous]
    [HttpGet]
    public IActionResult Get() => Ok(new { status = "ok", service = "dms-backend" });
}
