# WurmModLoader Documentation

Welcome to the WurmModLoader documentation!

## 🎉 Phase 6 Event Bus - NOW AVAILABLE!

**Modern annotation-driven event system for clean, powerful mod development**

### Quick Links
- **[Phase 6 Summary](PHASE6_SUMMARY.md)** - Start here for overview
- **[Event Bus Guide](EVENT_BUS_GUIDE.md)** - Complete API reference
- **[Migration Guide](MIGRATION_GUIDE.md)** - Convert existing mods
- **[Test Results](../PHASE6_TEST_RESULTS.md)** - Verification proof

### What's New?

Replace this:
```java
public class MyMod implements WurmServerMod, ServerStartedListener {
    @Override
    public void onServerStarted() { }
}
```

With this:
```java
public class MyMod implements WurmServerMod {
    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) { }
}
```

**Benefits:** Clean code, type safety, priority control, event cancellation, 100% backward compatible!

**Example:** See `examples/oversizedclub/` for a complete working mod.

---

## Documentation Status

📝 **Phase 6: ✅ Complete** - Event Bus system fully documented
📝 **Other Phases: Under Development** - Documentation being written as part of modernization plan

## Planned Documentation

### Getting Started
- Installation Guide
- Your First Mod
- Understanding Events
- Working with Registries

### Guides
- Creating Custom Items
- Custom Actions and Interactions
- Creature Templates and Behaviors
- Custom Spells
- Structure System
- Advanced Bytecode Manipulation
- Performance Optimization
- Debugging Your Mods

### Migration
- Migrating from WurmServerModLauncher
- API Changes and Compatibility
- Common Migration Issues

### Reference
- Event Reference
- Registry Reference
- Annotation Reference
- Configuration System
- CLI Tools Reference

### Examples
- Basic Item Mod
- Custom Creature
- Action System Example
- Complex Gameplay Mod

## Contributing to Documentation

We welcome documentation contributions! See [CONTRIBUTING.md](../CONTRIBUTING.md) for guidelines.

Documentation should be:
- Clear and concise
- Include code examples
- Provide screenshots where helpful
- Be kept up to date with code changes

## Questions?

For questions about using WurmModLoader:
- Check the [GitHub Discussions](https://github.com/garward/WurmModLoader/discussions)
- Open an [issue](https://github.com/garward/WurmModLoader/issues)
- Refer to the [original WurmServerModLauncher wiki](https://github.com/ago1024/WurmServerModLauncher/wiki) for legacy API reference
