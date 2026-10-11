# AcademyCraft Reborn — license notice

AcademyCraft Reborn is an **unofficial** port of AcademyCraft 1.1.3 to Fabric for Minecraft 26.3.
It is not affiliated with or endorsed by Lambda Innovation.

## AcademyCraft

AcademyCraft is created by **Lambda Innovation** (https://github.com/LambdaInnovation/AcademyCraft).
All versions of AcademyCraft, including this port, are licensed under the
[GNU GPL v3](https://www.gnu.org/licenses/gpl-3.0.html) (see `LICENSE_AcademyCraftReborn`),
with the following additional terms from the original authors:

> Prohibits any person, company, business, organization, etc. from selling AcademyCraft and its contents in any
> form, including but not limited to paid downloads (including but not limited to various legal currencies, virtual
> currency, game token, etc.) AcademyCraft's items, the sale of AcademyCraft ability within the game, etc.
>
> Lambda Innovation retains the copyright, the right of authorship, the ownership, etc. of AcademyCraft.

This port is distributed free of charge and must stay that way.

## LambdaLib2

The bundled `cn.lambdalib2` package is a port of LambdaLib2 by Lambda Innovation, licensed under the MIT license
(see `LICENSE_LambdaLib2`).

## Changes in this port

- Ported from Forge 1.12.2 to Fabric (Minecraft 26.3, Mojang mappings); Scala code rewritten in Java.
- Rendering moved from fixed-function OpenGL / custom shaders to the modern render pipeline.
- Removed: anonymous usage analytics, IC2 / RF / JEI / CraftTweaker / Bukkit integrations.
