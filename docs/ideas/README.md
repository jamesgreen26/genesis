# Ideas For Genesis

<ul>
{% for page in site.pages %}
  {% if page.url contains '/ideas/' and page.url != '/ideas/' %}
    {% assign filename = page.url | split: '/' | last | replace: '.md', '' | replace: '.html', '' %}
    <li>
      <a href="{{ page.url | relative_url }}">
        {{ filename | replace: '-', ' ' | replace: '%20', ' ' }}
      </a>
    </li>
  {% endif %}
{% endfor %}
</ul>
