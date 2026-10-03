# my-veloren — Unity / Android / Offline

> **الحالة: خارطة تطوير فقط — ليست لعبة أو مشروع Unity جاهزاً.**
> بناءً على توجيه المالك بتاريخ 2026-10-03، لا يبدأ التنفيذ أو البناء أو استيراد الأصول
> قبل موافقة جديدة. لا توجد APK ولا اختبارات Unity أو هاتف لهذا المشروع.

الهدف المستقبلي: إعادة تنفيذ تجربة Veloren على Unity لأندرويد، تعمل محلياً دون حساب
أو اتصال، مع خط استيراد يشمل مكتبة الأصول كاملة، وتعليمات قابلة للتكرار لأي مطور أو
وكيل برمجي. **نقل الخامات لا ينقل العالم والذكاء الاصطناعي والقتال والحركة تلقائياً.**

## ابدأ هنا

1. [خارطة التطوير والمراحل](docs/ROADMAP.md) — النطاق، الاعتماديات، وبوابات القبول.
2. [تعليمات المطورين والوكلاء](AGENTS.md) — قواعد العمل وحالة الصلاحية الحالية.
3. [المعمارية المقترحة](docs/ARCHITECTURE.md) — Unity، المحاكاة المحلية، العالم والحفظ.
4. [خطة استيراد جميع الأصول](docs/ASSET-PIPELINE.md) — voxel، خامات، صوت، بيانات، حركة.
5. [البناء والاختبار](docs/BUILD-AND-QA.md) — خطة بيئة قابلة للتكرار، لا أوامر منفذة.
6. [مصفوفة التغطية](docs/PARITY-MATRIX.md) — ما ينبغي تنفيذه وما لم يبدأ.
7. [المصادر والقرارات](docs/RESEARCH-AND-DECISIONS.md) — الأدلة وما يحتاج إثباتاً.

## ما تم فعلاً؟

- فحص بنية المصدر العام عند commit محدد، وجرد **بيانات شجرة الملفات**.
- تحديد الفروق بين استيراد الملفات، تحويلها إلى Unity، وتشغيلها داخل اللعبة.
- استخلاص دروس عامة من تجارب المالك السابقة في LibreQuake وXonotic وREKKR.
- كتابة خطة العمل ومعايير التحقق؛ **لم تُنسخ الأصول ولم تُنفَّذ اللعبة**.

الجرد الأولي: **10,654 ملفاً تحت `assets/`** في النسخة المرجعية، منها 4,752 ملف
`.vox` و898 صورة `.png` و2,855 ملف `.ron`. هذه أعداد ملفات المصدر، وليست أعداد
أصول مستوردة أو شخصيات مكتملة. راجع [تفاصيل الجرد](docs/ASSET-PIPELINE.md).

## English summary

This is a **documentation-only planning repository**, not a playable port, Unity project,
asset mirror or APK. The proposed product is an offline Android adaptation using Unity.
Implementation is explicitly paused until the owner authorizes a milestone.

Read [AGENTS.md](AGENTS.md) before editing. The requested lowercase
[agent.md](agent.md) points to the same canonical instructions.
All build entry points, paths and acceptance budgets described in the documents are
**proposals**, unless explicitly identified as verified upstream facts.

Veloren is an independent upstream project. This repository does not imply endorsement.
No upstream code or game assets are distributed in this planning revision.
