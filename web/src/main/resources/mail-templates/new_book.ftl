<#ftl output_format="HTML" auto_esc=true>
<#-- HTML email; MVC pages remain on Thymeleaf. -->
<#include "layout/header.ftl">
<h2 style="font-size:24px;color:#173b5e;margin:8px 0 22px;">Нова книга додана до каталогу</h2>
<div style="background:#eef6ff;border:1px solid #b8d4ef;border-radius:6px;padding:20px;margin-bottom:20px;">
  <p style="margin:0 0 12px;">Назва: <b>${title}</b></p>
  <p style="margin:0 0 12px;">Автор: <b>${author}</b></p>
  <p style="margin:0;">Рік видання: <b>${year?c}</b></p>
  <#if year lt 2000>
    <p style="color:#7a4e00;margin-bottom:0;"><b>Раритетне видання!</b></p>
  </#if>
</div>
<#if comments?? && comments?size gt 0>
<p>Коментарі:</p><ul><#list comments as c><li>${c}</li></#list></ul>
</#if>
<p style="color:#526175;">Дата додавання: ${added?string("yyyy-MM-dd HH:mm")}</p>
<p style="margin:26px 0;"><a href="${baseUrl}/comments?bookId=${id?c}"
   style="background:#235a86;color:#fff;padding:12px 18px;text-decoration:none;border-radius:5px;">Переглянути книгу</a></p>
<#include "layout/footer.ftl">
