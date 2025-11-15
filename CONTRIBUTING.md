# Contributing to WurmModLoader

Thank you for your interest in contributing to WurmModLoader! This document provides guidelines and information for contributors.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [How Can I Contribute?](#how-can-i-contribute)
- [Development Setup](#development-setup)
- [Coding Standards](#coding-standards)
- [Commit Guidelines](#commit-guidelines)
- [Pull Request Process](#pull-request-process)
- [Testing](#testing)
- [Documentation](#documentation)

## Code of Conduct

This project adheres to a code of conduct that all contributors are expected to follow:

- Be respectful and inclusive
- Welcome newcomers and help them learn
- Focus on what is best for the community
- Show empathy towards other community members
- Constructive criticism is welcome; personal attacks are not

## How Can I Contribute?

### Reporting Bugs

Before creating bug reports, please check existing issues to avoid duplicates.

**Good bug reports include:**
- A clear, descriptive title
- Steps to reproduce the behavior
- Expected vs actual behavior
- Screenshots or logs if applicable
- Your environment (OS, Java version, Wurm version)

### Suggesting Features

We love feature suggestions! Please:
- Check if the feature has already been suggested
- Provide a clear description of the feature
- Explain why this feature would be useful
- Consider backward compatibility implications

### Contributing Code

Areas where we need help:
- 🧪 Testing legacy mod compatibility
- 📚 Writing documentation and tutorials
- 🐛 Fixing bugs
- ✨ Implementing new features
- 🔧 Improving build/tooling
- 📝 Adding code examples

## Development Setup

### Prerequisites

- **Java 17 or later** (GraalVM recommended)
- **Git**
- **IDE** (IntelliJ IDEA recommended)

### Setup Steps

1. **Fork and clone the repository**
   ```bash
   git clone https://github.com/YOUR_USERNAME/WurmModLoader.git
   cd WurmModLoader
   ```

2. **Build the project**
   ```bash
   ./gradlew build
   ```

3. **Run tests**
   ```bash
   ./gradlew test
   ```

4. **Import into IDE**
   - IntelliJ IDEA: Open the root `build.gradle.kts` file
   - Eclipse: Import as Gradle project
   - VSCode: Open folder and use Java extension pack

### Project Structure

```
wurmmodloader/
├── wurmmodloader-api/         # Public API (stable)
├── wurmmodloader-core/        # Core implementation
├── wurmmodloader-legacy/      # Backward compatibility
├── wurmmodloader-modsupport/  # Mod utilities
├── wurmmodloader-patcher/     # Bytecode patcher
└── wurmmodloader-cli/         # Command-line tools
```

## Coding Standards

### Java Style

We follow standard Java conventions with some specifics:

- **Indentation**: 4 spaces (no tabs)
- **Line length**: 120 characters max
- **Braces**: K&R style (opening brace on same line)
- **Naming**:
  - Classes: `PascalCase`
  - Methods/variables: `camelCase`
  - Constants: `UPPER_SNAKE_CASE`
  - Packages: `lowercase`

### Code Quality

- Write clean, readable code
- Add JavaDoc for public APIs
- Include unit tests for new features
- Avoid unnecessary complexity
- Use meaningful variable names
- Keep methods focused and short

### Example

```java
package com.garward.wurmmodloader.api.event;

/**
 * Represents an event that can be posted to the event bus.
 *
 * <p>Events are used to notify mods about game state changes
 * and allow them to react or modify behavior.
 *
 * @see EventBus
 * @since 1.0.0
 */
public abstract class Event {

    private final boolean cancellable;
    private boolean cancelled = false;

    /**
     * Creates a new event.
     *
     * @param cancellable whether this event can be cancelled
     */
    protected Event(boolean cancellable) {
        this.cancellable = cancellable;
    }

    /**
     * Checks if this event can be cancelled.
     *
     * @return true if cancellable, false otherwise
     */
    public boolean isCancellable() {
        return cancellable;
    }

    // ... more methods
}
```

## Commit Guidelines

### Commit Message Format

```
<type>(<scope>): <subject>

<body>

<footer>
```

### Types

- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes (formatting, etc.)
- `refactor`: Code refactoring
- `test`: Adding or updating tests
- `chore`: Build process, dependencies, etc.

### Examples

```
feat(event): add ItemUseEvent for item interactions

Adds a new cancellable event that fires when a player uses an item.
This allows mods to intercept and modify item usage behavior.

Closes #123
```

```
fix(registry): prevent duplicate registration errors

Fixed an issue where registering the same key twice would cause
a runtime exception instead of a clear error message.
```

## Pull Request Process

1. **Create a feature branch**
   ```bash
   git checkout -b feature/my-awesome-feature
   ```

2. **Make your changes**
   - Write code following our standards
   - Add tests for new functionality
   - Update documentation as needed

3. **Commit your changes**
   ```bash
   git add .
   git commit -m "feat(scope): descriptive message"
   ```

4. **Push to your fork**
   ```bash
   git push origin feature/my-awesome-feature
   ```

5. **Create a pull request**
   - Use a clear, descriptive title
   - Reference any related issues
   - Describe what your PR does
   - Include screenshots/logs if relevant

6. **Wait for review**
   - Address any feedback promptly
   - Keep your branch up to date with main
   - Be patient and respectful

### PR Checklist

Before submitting, ensure:
- [ ] Code follows style guidelines
- [ ] All tests pass (`./gradlew test`)
- [ ] New features have tests
- [ ] Documentation is updated
- [ ] Commit messages follow guidelines
- [ ] No merge conflicts
- [ ] JavaDoc is complete for public APIs

## Testing

### Running Tests

```bash
# Run all tests
./gradlew test

# Run tests for specific module
./gradlew :wurmmodloader-core:test

# Run with coverage
./gradlew test jacocoTestReport
```

### Writing Tests

Use JUnit 5 and AssertJ:

```java
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class EventBusTest {

    @Test
    void shouldDispatchEventToSubscribers() {
        EventBus bus = new EventBus();
        TestListener listener = new TestListener();

        bus.register(listener);
        bus.post(new TestEvent());

        assertThat(listener.called).isTrue();
    }
}
```

### Test Coverage

- Aim for 80%+ coverage for new code
- Focus on testing public APIs
- Include edge cases and error conditions
- Use mocks for external dependencies

## Documentation

### JavaDoc

All public APIs must have complete JavaDoc:

```java
/**
 * Brief description of what this does.
 *
 * <p>Longer explanation with more details.
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * Example code here
 * }</pre>
 *
 * @param paramName description of parameter
 * @return description of return value
 * @throws ExceptionType when this exception occurs
 * @see RelatedClass
 * @since 1.0.0
 */
public ReturnType methodName(ParamType paramName) {
    // implementation
}
```

### Markdown Documentation

- Use clear, concise language
- Include code examples
- Add screenshots where helpful
- Keep documentation up to date with code

### Documentation Locations

- **API docs**: Inline JavaDoc
- **Guides**: `docs/guides/`
- **Examples**: `examples/`
- **README**: High-level overview

## Questions?

- **General questions**: [GitHub Discussions](https://github.com/garward/WurmModLoader/discussions)
- **Bug reports**: [GitHub Issues](https://github.com/garward/WurmModLoader/issues)
- **Feature requests**: [GitHub Issues](https://github.com/garward/WurmModLoader/issues)

## Recognition

Contributors will be recognized in:
- CONTRIBUTORS.md file
- Release notes
- Project README

Thank you for contributing to WurmModLoader! 🎉
