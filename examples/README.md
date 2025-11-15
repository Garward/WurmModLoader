# WurmModLoader Examples

This directory contains example mods demonstrating various features of WurmModLoader.

## Available Examples

### Planned Examples

1. **basic-item-mod/** - Simple mod adding a custom item
   - Item template creation
   - Registry usage
   - Basic configuration

2. **custom-creature/** - Creating and spawning custom creatures
   - Creature template builder
   - AI behavior
   - Spawning mechanics

3. **action-system/** - Custom player actions
   - Action registration
   - Action performers
   - Player interaction

4. **spell-mod/** - Custom spell system
   - Spell creation
   - Effect implementation
   - Target validation

5. **complex-gameplay/** - Full-featured gameplay mod
   - Multiple systems integration
   - Configuration management
   - Player data persistence
   - Admin commands

## Status

🚧 **Under Development** - Examples will be created during Phase 9 (Documentation & Developer Experience)

## Using Examples

Once available, each example will include:
- Complete source code
- build.gradle.kts for building
- README with explanation
- Comments explaining key concepts

To use an example:

```bash
cd examples/basic-item-mod
./gradlew build
cp build/libs/*.jar /path/to/wurm/mods/
```

## Contributing Examples

Have a great example mod? We'd love to include it! See [CONTRIBUTING.md](../CONTRIBUTING.md) for guidelines.

Good examples:
- Demonstrate a specific feature clearly
- Include thorough comments
- Follow best practices
- Are simple and focused
- Include a detailed README
