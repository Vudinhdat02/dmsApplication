// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Vudinhdat02

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
