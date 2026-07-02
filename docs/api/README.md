# Celestial API Documentation

Documentation for adding and customizing celestials in the Genesis mod.

## Contents

- **[Getting Started](getting-started.md)** — Core concepts and quick start
- **[Implementation Guide](implementation-guide.md)** — Adding celestials via datapacks, with examples and property reference
- **[Advanced Topics](advanced.md)** — Custom types, renderers, and transform providers
- **[API Reference](api-reference.md)** — Full class and method reference

## Quick Links

- [Genesis Built-in Celestials](https://github.com/jamesgreen26/genesis/tree/1.20.1/src/main/resources/data/genesis/genesis/celestials) — Example JSON files
- [Misode Data Generator](https://cosmic-mod-generator.github.io/genesis/celestials/) - Gui-based datapack generator by ```@Brickyboy```

---

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
