// Host-side unit tests for voxygen/src/touch.rs (patch 0002). `run.sh` links the
// file from the patched upstream checkout into this crate; nothing else is needed.
pub mod touch;
// dev.4: GPU → graphics tier classification (voxygen/src/mobile_gpu.rs, patch 0003).
pub mod mobile_gpu;

#[cfg(test)]
mod mobile_gpu_tests {
    use crate::mobile_gpu::{GpuClass::*, class};

    #[test]
    fn adreno_tiers() {
        assert_eq!(class("Adreno (TM) 650"), Baseline); // r8q, Galaxy S20 FE
        assert_eq!(class("Adreno (TM) 725"), Baseline);
        assert_eq!(class("Adreno (TM) 730"), Strong);
        assert_eq!(class("Adreno (TM) 750"), Strong); // e3q, Galaxy S24 Ultra
        assert_eq!(class("Adreno (TM) 830"), Strong);
        assert_eq!(class("Adreno (TM) 610"), Baseline);
    }

    #[test]
    fn arm_and_others() {
        assert_eq!(class("Mali-G78 MP14"), Baseline);
        assert_eq!(class("Mali-G710 MC10"), Strong);
        assert_eq!(class("Mali-G615 MC2"), Baseline);
        assert_eq!(class("Immortalis-G720 MC12"), Strong);
        assert_eq!(class("Samsung Xclipse 940"), Strong);
        assert_eq!(class("PowerVR B-Series BXM-8-256"), Baseline);
        assert_eq!(class("llvmpipe (LLVM 17.0.6, 128 bits)"), Baseline);
        assert_eq!(class(""), Baseline);
    }
}
