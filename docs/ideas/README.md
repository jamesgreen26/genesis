# Ideas For Genesis

<ul>
{% for page in site.pages %}
  {% comment %}
    Include only pages whose URL starts with /ideas/
    Exclude the index page itself
  {% endcomment %}
  {% if page.url != "/ideas/" and page.url startswith "/ideas/" %}
    {% assign filename = page.url | split: '/' | last | replace: '.md', '' | replace: '.html', '' %}
    <li>
      <a href="{{ page.url | relative_url }}">
        {{ filename | replace: '-', ' ' }}
      </a>
    </li>
  {% endif %}
{% endfor %}
</ul>

