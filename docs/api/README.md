# Celestial API Documentation

Comprehensive documentation for adding and customizing celestials in the Genesis mod.

## Getting Started

New to celestials? Start here to understand the basics:

- **[Getting Started](getting-started.md)** - Introduction to celestials, core concepts, and quick examples

## Guides

Step-by-step guides for adding celestials:

- **[Datapack Guide](datapack-guide.md)** - Add celestials using JSON datapacks (recommended for most users)
  - File structure and JSON format
  - Progressive examples from simple to complex
  - Complete property reference

- **[Code Guide](code-guide.md)** - Register celestials programmatically in Java (for mod developers)
  - Event registration system
  - Creating celestials in code
  - Working with transform providers

## Advanced Topics

Extend Genesis with custom functionality:

- **[Advanced Topics](advanced.md)** - Create custom celestial types and transform providers
  - Custom celestial types with unique rendering
  - Custom orbital mechanics and transform providers
  - Codec registration for datapack support

## Reference

Complete API documentation:

- **[API Reference](api-reference.md)** - Full API specification
  - All classes, methods, and properties
  - Constructor signatures
  - JSON schema documentation
  - Built-in types and constants

## Quick Links

- [Genesis Built-in Configuration](../../src/main/resources/data/genesis/system_config/builtin.json) - Example solar system
- [Registration Entry Point](../../src/main/java/shipwrights/genesis/GenesisMod.java) - Code registration hook

---

## All Pages

<ul>
{% for page in site.pages %}
  {% if page.url contains '/api/' and page.url != '/api/' %}
    {% assign filename = page.url | split: '/' | last | replace: '.md', '' | replace: '.html', '' %}
    <li>
      <a href="{{ page.url | relative_url }}">
        {{ filename | replace: '-', ' ' | replace: '%20', ' ' }}
      </a>
    </li>
  {% endif %}
{% endfor %}
</ul>