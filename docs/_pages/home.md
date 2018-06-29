---
layout: splash
permalink: /
header:
  overlay_color: "#5e616c"
  overlay_image: /assets/images/background.jpg
  cta_label: "<i class='fas fa-download'></i> Install Now"
  cta_url: "/docs/installation/"
  caption:
excerpt: 'Distribute FITS data with Apache Spark: Binary tables, images and more!<br /> <small><a href="https://github.com/theastrolab/spark-fits/releases/tag/0.4.0">Latest release v0.4.0</a></small><br /><br /> {::nomarkdown}<iframe style="display: inline-block;" src="https://ghbtns.com/github-btn.html?user=theastrolab&repo=spark-fits&type=star&count=true&size=large" frameborder="0" scrolling="0" width="160px" height="30px"></iframe> <iframe style="display: inline-block;" src="https://ghbtns.com/github-btn.html?user=theastrolab&repo=spark-fits&type=fork&count=true&size=large" frameborder="0" scrolling="0" width="158px" height="30px"></iframe>{:/nomarkdown}'
feature_row:
  - image_path:
    alt:
    title: "<i class='fas fa-upload'></i> Load 3D object RDD"
    excerpt: "Distribute points, spheres, shells, boxes, and more using spark3D."
    url: "/docs/introduction/"
    btn_class: "btn--primary"
    btn_label: "Learn More"
  - image_path:
    alt:
    title: "<i class='fas fa-cubes'></i> Partition your space"
    excerpt: "Partition the three-dimensional space to speed-up your search."
    url: "/docs/partitioning/"
    btn_class: "btn--primary"
    btn_label: "Learn More"
  - image_path:
    alt:
    title: "<i class='fas fa-crosshairs'></i> Query, match, play!"
    excerpt: "Find objects based on conditions, cross-match data sets, and define your requests."
    url: "/docs/query/"
    btn_class: "btn--primary"
    btn_label: "Learn More"
---

{% include feature_row id="intro" type="center" %}

{% include feature_row %}