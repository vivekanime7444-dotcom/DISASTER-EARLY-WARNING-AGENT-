/**
 * Web Audio API Sound Synthesizer.
 * Provides reliable, zero-asset emergency sound generation directly in the browser:
 * 1. Warning Chime (Soft advisory chime)
 * 2. Critical Klaxon Siren (Pulsed two-tone emergency alarm)
 * Complies with browser user-gesture and silent-mode policies.
 */

class SoundSynthesizer {
    constructor() {
        this.audioCtx = null;
        this.isMuted = false;
        this.isPlayingSiren = false;
        this.sirenInterval = null;
    }

    init() {
        if (!this.audioCtx) {
            const AudioContext = window.AudioContext || window.webkitAudioContext;
            if (AudioContext) {
                this.audioCtx = new AudioContext();
            }
        }
        if (this.audioCtx && this.audioCtx.state === 'suspended') {
            this.audioCtx.resume();
        }
    }

    setMuted(muted) {
        this.isMuted = muted;
        if (muted) {
            this.stopSiren();
        }
    }

    toggleMute() {
        this.setMuted(!this.isMuted);
        return this.isMuted;
    }

    /**
     * Plays a pleasant 2-tone notification chime for weather alerts or normal updates.
     */
    playChime() {
        if (this.isMuted) return;
        this.init();
        if (!this.audioCtx) return;

        const now = this.audioCtx.currentTime;
        const osc = this.audioCtx.createOscillator();
        const gain = this.audioCtx.createGain();

        osc.type = 'sine';
        osc.frequency.setValueAtTime(587.33, now); // D5
        osc.frequency.exponentialRampToValueAtTime(880.0, now + 0.15); // A5

        gain.gain.setValueAtTime(0.01, now);
        gain.gain.linearRampToValueAtTime(0.3, now + 0.05);
        gain.gain.exponentialRampToValueAtTime(0.001, now + 0.6);

        osc.connect(gain);
        gain.connect(this.audioCtx.destination);

        osc.start(now);
        osc.stop(now + 0.65);
    }

    /**
     * Plays an authoritative emergency klaxon tone for critical disaster warnings within 100km.
     */
    playCriticalAlarm(durationSeconds = 3) {
        if (this.isMuted) return;
        this.init();
        if (!this.audioCtx) return;

        this.stopSiren();
        this.isPlayingSiren = true;

        const now = this.audioCtx.currentTime;
        const osc = this.audioCtx.createOscillator();
        const gain = this.audioCtx.createGain();

        osc.type = 'sawtooth';

        // Modulate frequency between 750Hz and 1100Hz (standard disaster siren pattern)
        osc.frequency.setValueAtTime(750, now);
        for (let i = 0; i < durationSeconds; i++) {
            osc.frequency.linearRampToValueAtTime(1100, now + i + 0.4);
            osc.frequency.linearRampToValueAtTime(750, now + i + 0.8);
        }

        gain.gain.setValueAtTime(0.01, now);
        gain.gain.linearRampToValueAtTime(0.35, now + 0.1);
        gain.gain.setValueAtTime(0.35, now + durationSeconds - 0.2);
        gain.gain.exponentialRampToValueAtTime(0.001, now + durationSeconds);

        osc.connect(gain);
        gain.connect(this.audioCtx.destination);

        osc.start(now);
        osc.stop(now + durationSeconds + 0.1);

        setTimeout(() => {
            this.isPlayingSiren = false;
        }, durationSeconds * 1000);
    }

    stopSiren() {
        this.isPlayingSiren = false;
        if (this.sirenInterval) {
            clearInterval(this.sirenInterval);
            this.sirenInterval = null;
        }
    }
}

window.soundEngine = new SoundSynthesizer();
