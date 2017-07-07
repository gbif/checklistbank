<?xml version='1.0' encoding='UTF-8'?>
<sitemapindex xmlns='http://www.sitemaps.org/schemas/sitemap/0.9'>
<#list 1..maps as i>
    <sitemap><loc>https://api.gbif.org/v1/sitemap/species/${i?c}.txt</loc></sitemap>
</#list>
</sitemapindex>
