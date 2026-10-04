//! my-veloren dev.8 (8.2): generate a small world map on the build machine.
//! Copied into `$VELOREN_SRC/world/examples/` by `tools/pregen_worlds.sh` and run
//! as `cargo run -p veloren-world --example my_veloren_pregen -- <out.bin> <seed>
//! <x_lg> <y_lg> <circle|square> [threads]`. Prints the generation time, which is
//! also the yardstick for "generate on the phone" limits.
use common::resources::MapKind;
use std::time::Instant;
use veloren_world::{
    World,
    sim::{FileOpts, GenOpts, WorldOpts},
};

fn main() {
    let a: Vec<String> = std::env::args().collect();
    if a.len() < 6 {
        eprintln!("usage: <out.bin> <seed> <x_lg> <y_lg> <circle|square> [threads]");
        std::process::exit(2);
    }
    let out = a[1].clone();
    let seed: u32 = a[2].parse().expect("seed");
    let x_lg: u32 = a[3].parse().expect("x_lg");
    let y_lg: u32 = a[4].parse().expect("y_lg");
    let map_kind = if a[5] == "circle" { MapKind::Circle } else { MapKind::Square };
    let threads: usize = a.get(6).and_then(|t| t.parse().ok()).unwrap_or(0);
    let mut b = rayon::ThreadPoolBuilder::new();
    if threads > 0 {
        b = b.num_threads(threads);
    }
    let pool = b.build().expect("thread pool");
    let t = Instant::now();
    let (world, index) = World::generate(
        seed,
        WorldOpts {
            seed_elements: true,
            world_file: FileOpts::Save(out.clone().into(), GenOpts {
                x_lg,
                y_lg,
                map_kind,
                ..GenOpts::default()
            }),
            calendar: None,
        },
        &pool,
        &|_| {},
    );
    let secs = t.elapsed().as_secs_f32();
    let sites = index.sites.iter().count();
    let mut kinds = std::collections::BTreeMap::new();
    for (_, site) in index.sites.iter() {
        let k = site.kind.as_ref().map(|k| format!("{k:?}")).unwrap_or_else(|| "?".into());
        let k = k.split('(').next().unwrap_or("?").to_string();
        *kinds.entry(k).or_insert(0u32) += 1;
    }
    core::hint::black_box(&world);
    println!(
        "PREGEN out={out} seed={seed} size={x_lg}x{y_lg} kind={map_kind:?} threads={} sites={sites} secs={secs:.1} kinds={kinds:?}",
        pool.current_num_threads()
    );
}
