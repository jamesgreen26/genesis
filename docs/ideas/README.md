# Ideas For Genesis

{% assign idea_pages = site.pages | where: "dir", "/ideas/" %}
<ul>
{% for page in idea_pages %}
  {% if page.url != "/ideas/" %}
    <li>
      <a href="{{ page.url | relative_url }}">
        {{ page.title | default: page.name | replace: ".md", "" }}
      </a>
    </li>
  {% endif %}
{% endfor %}
</ul>
