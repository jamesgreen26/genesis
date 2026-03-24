# Celestial API Documentation

Comprehensive documentation for adding and customizing celestials in the Genesis mod.

## Getting Started

New to celestials? Start here to understand the basics:

- **[Getting Started](getting-started.md)** - Introduction to celestials, core concepts, and quick examples

## Guides

Step-by-step guide for adding celestials:

- **[Implementation Guide](implementation-guide.md)** - Add celestials using datapacks (JSON) or code (Java)
  - Shows both datapack and code approaches side-by-side
  - Progressive examples from simple to complex
  - Complete property reference for both formats
  - Setup instructions for datapacks and mod development

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

- [Genesis Built-in Configuration](https://github.com/jamesgreen26/genesis/tree/1.20.1/src/main/resources/data/genesis/system_config) - Example solar system
- [Registration Entry Point](https://github.com/jamesgreen26/genesis/blob/7be2f2ddb6d2b93bc7abd8425f14626374a8bc25/src/main/java/shipwrights/genesis/GenesisMod.java#L109) - Code registration hook

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