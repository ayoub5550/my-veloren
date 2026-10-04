# بحث: هل توجد تقنية أو محرك «يطبخ» اللعبة لأندرويد بسهولة ويجعلها أخف؟

> 2026-10-03 · بطلب من المالك («ابحث في الإنترنت في كل ورقة بحثية ممكنة»).
> بحث على الويب (أوراق محكّمة، توثيق Google/Qualcomm/Arm، مشاريع مفتوحة). هذا مرجع للقرار، وليس مرحلة منفّذة.
> حجم Veloren عند الـpin `585a91b4`: **~410 ألف سطر Rust** و10,654 ملف موارد (444MB).

## الخلاصة في سطرين

لا يوجد محرك أو أداة «تطبخ» لعبة مكتوبة بمحرك خاص (Veloren/Rust) إلى أندرويد تلقائيًا. الطرق الموجودة أربع
عائلات، ونسختنا الأصلية (native) هي أخفّها على الهاتف. تقنيات «التخفيف» الحقيقية من الأبحاث تُضاف **فوق** النسخة الأصلية
(البند 5)، ولا تتطلب تغيير المحرك.

## 1) ترجمة الكود آليًا (ذكاء اصطناعي / transpilers) → Unity/C#

| المصدر | النتيجة |
|---|---|
| **AlphaTrans** (arXiv 2410.24117، FSE 2025): ترجمة مستودعات كاملة Java→Python | 96.4% من الأجزاء صحيحة نحويًا، لكن **25–27% فقط صحيحة وظيفيًا**؛ ~34 ساعة آلية + ~20 ساعة إصلاح بشري لكل مشروع (مشاريع أصغر بكثير من Veloren) |
| **Oxidizer** (ACM 10.1145/3729315، 2025): Go→Rust | 73% من الدوال صحيحة، لكن لمشاريع **حتى 9.7 ألف سطر** فقط؛ الدقة تنهار فوق ~100 سطر بدون تقسيم |
| **WIPI-to-Android converter** (Dongguk Univ.) | محوّل ألعاب آلي ينجح فقط بين منصّتين متشابهتين جدًا (API mapping) |

**الحكم:** Veloren أكبر بـ40 مرة من أكبر مشروع نجح في الأبحاث. الترجمة الآلية إلى C#/Unity غير عملية؛ ستنتج لعبة مكسورة.

## 2) طبقات توافق: تشغيل نسخة الكمبيوتر كما هي على الهاتف

| الأداة | كيف تعمل | العيب |
|---|---|---|
| **Winlator / Mobox / GameNative** | Wine + Box64/FEX (ترجمة x86→ARM) + Turnip (تعريف Vulkan مفتوح لـAdreno) | أبطأ من native، يحتاج إعدادًا كثيرًا، الأفضل على Adreno 7xx (XDA) |
| **DroidDeck 0.3.0** (صدر 2026-10-03) | Steam ARM64 + Proton ARM64 داخل proot Linux + gamescope | يحتاج **Adreno 730+** و~3GB للبيئة، أثقل من تطبيقنا |
| **PojavLauncher** (Minecraft Java) | JVM + GL4ES/MobileGlues تترجم OpenGL إلى GLES | مثال ناجح لكنه طبقة ترجمة إضافية تكلّف أداءً |

**الحكم:** كلها تحاول الوصول إلى ما فعلناه مباشرة (كود ARM64 أصلي + Vulkan). نسختنا أخفّ من أي منها.

## 3) البث (Streaming)

Moonlight + Sunshine، Steam Link (كودك **Pyrowave** التجريبي، Digital Foundry 2026-09). الهاتف لا يعمل تقريبًا شيئًا، لكن
يحتاج كمبيوترًا قويًا شغّالًا وشبكة دائمًا. ليس تطبيقًا مستقلًا ولا offline.

## 4) الويب (WASM + WebGPU)

wgpu يدعم المتصفح، لكن Veloren يعتمد على الخيوط ونظام الملفات والخادم المحلي؛ ولا يوجد دعم wasm رسمي. الأداء في متصفح الهاتف
أقل من native. **غير مناسب.**

## 5) ما يجعل النسخة الأصلية أخف فعلًا (مرشّح للتنفيذ)

| التقنية | المصدر | الفائدة المتوقعة لـVeloren |
|---|---|---|
| **Cave/occlusion culling** على مستوى الـchunk | Tommo (Mojang)، «Advanced Cave Culling Algorithm» لـMinecraft PE 0.9: حذف **50–99%** من الهندسة، سمح بالكهوف على كل الهواتف | قد يكون أكبر مكسب للـGPU؛ يجب أولًا فحص ما يفعله Voxygen حاليًا (frustum فقط؟) |
| **Snapdragon GSR / SGSR** (تكبير مكاني بتمريرة واحدة) | دليل Qualcomm لـAdreno: «ارسم بأقل دقة مقبولة ثم كبّر، فضّل SGSR» | بديل أوضح من FxUpscale عند دقة 0.5–0.6 |
| **Arm ASR** (تكبير زمني، مبني على FSR2) | Vulkanised 2025 | تكبير 2× = رسم 25% من البكسلات؛ يحتاج motion vectors (عمل أكبر) |
| **ADPF** (Thermal headroom + Performance Hint) | Android Developers | يخفض الإعدادات قبل أن يسخن الهاتف ويبطئ؛ أداء ثابت لجلسات طويلة |
| **Vulkan best practices لـAdreno/Mali** | Qualcomm/Arm | تقليل الـoverdraw والـbandwidth (مهم في GPU من نوع tile-based) |
| **Aokana** (SVDAG، GPU-driven voxel، ACM 2025) | ورقة بحثية | إعادة كتابة للعارض بالكامل؛ غير واقعي الآن، للاطلاع فقط |
| **تخفيف الحجم** | Google «Reduce game size»: كل +6MB = −1% تثبيت؛ Play Asset Delivery للألعاب >200MB | الموسيقى 273MB → ~70MB بإعادة الترميز، حذف خريطتين غير مستخدمتين، ضغط `assets.tar` → ~200MB |

## التوصية

1. نبقى على المسار الأصلي (ADR-002). لا Unity ولا طبقات توافق.
2. بعد قياس dev.4 على جهاز حقيقي: أضف للخارطة **(أ) occlusion culling**، **(ب) SGSR**، **(ج) ADPF**، **(د) تخفيف الحجم**،
   بهذا الترتيب حسب الفائدة/الجهد. كل بند يُقاس بتشغيل واحد على r8q.

## المراجع

- AlphaTrans: https://arxiv.org/html/2410.24117 · Oxidizer: https://dl.acm.org/doi/10.1145/3729315
- WIPI-to-Android: https://pure.dongguk.edu/en/publications/design-and-implementation-of-the-wipi-to-android-automatic-mobile-2/
- UI adaptation desktop→mobile (17 patterns): https://exa.ai/library/publication/2m4zdbkl7p1
- Mobox vs Winlator: https://www.xda-developers.com/mobox-vs-winlator/ · Winlator hub: https://github.com/Arihany/WinlatorWCPHub
- DroidDeck: https://droiddeck.app/ · https://www.notebookcheck.net/DroidDeck-0-3-0-brings-Steam-Deck-style-controls-and-frame-generation-to-Android-as-Steam-startup-falls-from-31-to-6-seconds.1414466.0.html
- PojavLauncher renderers: https://pojavlauncher.app/wiki/faq/android/RENDERERS.html
- Moonlight vs Steam Link 2026: https://shattered.io/moonlight-vs-steam-link/ · Pyrowave: https://www.digitalfoundry.net/news/2026/09/steam-client-beta-adds-pyrowave-for-high-bandwidth-low-latency-game-streaming
- wgpu on the web: https://wgpu.rs/doc/wgpu/documentation/platforms/web/index.html
- Minecraft PE cave culling: https://tomcc.github.io/2014/08/31/visibility-1.html
- Aokana: https://arxiv.org/html/2505.02017 · NanoMesh mobile (SIGGRAPH 2024): https://advances.realtimerendering.com/s2024/content/Cao-NanoMesh/AdavanceRealtimeRendering_NanoMesh0810.pdf
- Qualcomm Adreno best practices: https://docs.qualcomm.com/nav/home/mobile_best_practices.html?product=1601111740035277 · SGSR 2: https://www.qualcomm.com/developer/blog/2024/10/introducing-snapdragon-game-super-resolution-2
- Arm ASR: https://www.vulkan.org/user/pages/09.events/vulkanised-2025/T24-Sergio-Alapont-Arm-ArmASR.pdf
- ADPF: https://developer.android.com/games/optimize/adpf
- Game size / PAD: https://developer.android.com/games/optimize/game-size · https://developer.android.com/guide/playcore/asset-delivery
- Veloren performance guide: https://book.veloren.net/players/performance.html
