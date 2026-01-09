# API Documentation

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