<#ftl output_format="HTML" auto_esc=true>
<#-- Lab 9: e-mail confirmation of a new account (/confirm?token=...). -->
<#include "layout/header.ftl">
<h2 style="font-size:24px;color:#173b5e;margin:8px 0 22px;">Підтвердіть реєстрацію</h2>
<p style="margin:0 0 14px;">Для адреси <b>${email}</b> створено обліковий запис у каталозі книг.</p>
<p style="margin:0 0 14px;">Поки адресу не підтверджено, вхід неможливий: акаунт має роль REGISTERED.
  Після переходу за посиланням роль зміниться на USER.</p>
<p style="margin:26px 0;"><a href="${link}"
   style="background:#235a86;color:#fff;padding:12px 18px;text-decoration:none;border-radius:5px;">Підтвердити email</a></p>
<p style="color:#526175;font-size:13px;">Якщо кнопка не працює, відкрийте посилання: ${link}</p>
<p style="color:#526175;font-size:13px;">Якщо ви не реєструвалися, просто проігноруйте цей лист.</p>
<#include "layout/footer.ftl">
