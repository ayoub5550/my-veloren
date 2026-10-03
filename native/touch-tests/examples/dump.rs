use touch_tests::touch::*;
fn main() {
    let mode = std::env::args().nth(1).unwrap_or_default();
    let mut ctx = Context { in_session: true, game_input: true, ..Default::default() };
    let mut s = TouchSettings::default();
    let mut l = TouchLayer::default();
    let (w, h) = (2400.0, 1080.0);
    set_window_height(h);
    match mode.as_str() {
        "context" => { ctx.can_interact = true; ctx.can_mount = true; ctx.in_liquid = true; }
        "edit" => {
            let e = s.layout(Ctl::Edit);
            l.on_touch(1, Phase::Started, e.x as f64 * w, e.y as f64 * h, w, h, &ctx, &mut s);
            l.on_touch(1, Phase::Ended, e.x as f64 * w, e.y as f64 * h, w, h, &ctx, &mut s);
            let j = s.layout(Ctl::Jump);
            l.on_touch(2, Phase::Started, j.x as f64 * w, j.y as f64 * h, w, h, &ctx, &mut s);
        }
        _ => {
            // stick held + attack pressed
            l.on_touch(1, Phase::Started, 0.2 * w, 0.72 * h, w, h, &ctx, &mut s);
            l.on_touch(1, Phase::Moved, 0.2 * w + 60.0, 0.72 * h - 110.0, w, h, &ctx, &mut s);
            let p = s.layout(Ctl::Primary);
            l.on_touch(3, Phase::Started, p.x as f64 * w, p.y as f64 * h, w, h, &ctx, &mut s);
        }
    }
    for p in l.primitives(&ctx, &s, (w / h) as f32) {
        println!("{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}", p.x, p.y, p.r, p.rgba[0], p.rgba[1], p.rgba[2], p.rgba[3], p.label.unwrap_or(""));
    }
}
