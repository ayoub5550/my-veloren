# my-veloren — Unity / Android / Offline

> **الحالة: dev.9 (0.1.0-dev.9، فرع مراجعة) — يد تحكم، ولوحة مفاتيح وفأرة بلوتوث، واللعب الجماعي على خوادم Veloren الرسمية بحساب veloren.net مجاني (اللعب الفردي يبقى دون إنترنت). التفاصيل: [docs/DEV9.md](docs/DEV9.md).**
>
> **dev.8 (0.1.0-dev.8، مدموجة) — مراجعة محتوى العالم على الهاتف (مدن، زنزانات وزعماء، طقس، ليل ونهار، طيران شراعي، قوارب ومناطيد، حصاد)، وعوالم جاهزة تُشحن مع اللعبة، وعالم جديد بحجم آمن للهاتف، وعدة شخصيات، وتصدير الحفظ واستيراده. التفاصيل: [docs/DEV8.md](docs/DEV8.md).**
>
> **dev.7 (0.1.0-dev.7، مدموجة) — إصلاحات تجربة المالك على Poco F3: Ultra آمن للهاتف مع مراقب حرارة، أزرار بأيقونات بلا تداخل، عصا عائمة أنعم، قائمة رئيسية للمس، إعدادات تحكم باللمس فقط، وأيقونة جديدة. التفاصيل: [docs/DEV7.md](docs/DEV7.md).**
>
> **dev.5 (0.1.0-dev.5) — Veloren الأصلية (Rust) على أندرويد، تعمل دون إنترنت، مع تحكم لمس كامل، وإعدادات رسوميات للهاتف، ودورة حياة أندرويد سليمة (خروج وعودة، حفظ تلقائي، زر الرجوع، تقرير كراش)، وإخفاء التضاريس المحجوبة. التفاصيل: [docs/DEV5.md](docs/DEV5.md) · dev.4: [docs/DEV4.md](docs/DEV4.md).**
> المسار الأصلي native (ADR-002): عميل Voxygen الحقيقي مع خادم فردي مدمج، وكل الموارد، وخريطة العالم الرسمية.
> يتضمن dev.3 عصا تحكم تناظرية، وكاميرا بالسحب، وتكبيرًا بإصبعين، وأزرار القتال والحركة، وشريط قوائم، وأزرارًا حسب السياق،
> ومحرر تخطيط، واهتزازًا. جُرّب في Firebase Test Lab على جهاز حقيقي.
> التفاصيل: [docs/DEV3.md](docs/DEV3.md) · [docs/DEV2-NATIVE.md](docs/DEV2-NATIVE.md) · خارطة المراحل [docs/ROADMAP.md](docs/ROADMAP.md).
> شريحة Unity (dev1) مؤرشفة: [docs/DEV1.md](docs/DEV1.md).

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

Status: **dev.9 (native, ADR-002; review branch)** — gamepad, Bluetooth keyboard and mouse, and optional multiplayer on the official Veloren servers ([docs/DEV9.md](docs/DEV9.md)). dev.8 (merged): content audit on the phone, ready-made small worlds shipped in the APK, phone-safe new-world size, several characters, save export/import ([docs/DEV8.md](docs/DEV8.md)). dev.4–dev.7: mobile tiers, lifecycle, touch UI, owner phone fixes ([DEV7](docs/DEV7.md)). Base: the real upstream Veloren client (Voxygen, Rust) cross-compiled for arm64 Android with the complete asset tree (~444 MB) and the official world map, offline singleplayer, plus a full on-screen touch layer (analog stick, camera drag/pinch, combat/movement buttons, menu bar, context buttons, layout editor, haptics) tested with injected touches on Firebase Test Lab. See [docs/DEV3.md](docs/DEV3.md) and [docs/DEV2-NATIVE.md](docs/DEV2-NATIVE.md). dev1 (Unity slice, docs/DEV1.md) is archived.
Each further milestone starts only on an owner instruction (see docs/ROADMAP.md).

Read [AGENTS.md](AGENTS.md) before editing. The requested lowercase
[agent.md](agent.md) points to the same canonical instructions.
All build entry points, paths and acceptance budgets described in the documents are
**proposals**, unless explicitly identified as verified upstream facts.

Veloren is an independent upstream project. This repository does not imply endorsement.
No upstream code or game assets are distributed in this planning revision.
