(() => {
'use strict';
const video = document.getElementById('connect-film');
const button = document.getElementById('film-toggle');
if (!video || !button) return;
const reduced = window.matchMedia('(prefers-reduced-motion: reduce)');
const hero = video.closest('.connect-hero');
let loaded = false, userPaused = false, attempt = 0;
const label = () => { button.textContent = video.paused ? 'Phát video' : 'Tạm dừng video'; };
const load = () => {
  if (loaded) return;
  video.querySelectorAll('source[data-src]').forEach(source => { source.src = source.dataset.src; });
  loaded = true; video.load();
};
const play = async () => {
  const token = ++attempt; load();
  try { await video.play(); if (token !== attempt || reduced.matches || userPaused) video.pause(); }
  catch { hero.classList.remove('film-playing'); }
  label();
};
const applyPreference = () => {
  ++attempt;
  if (reduced.matches) { video.pause(); hero.classList.remove('film-playing'); button.hidden = true; }
  else { button.hidden = false; if (!userPaused) void play(); }
};
video.muted = true;
video.addEventListener('playing', () => { if (!reduced.matches) hero.classList.add('film-playing'); label(); });
video.addEventListener('pause', label);
const failed = () => { ++attempt; video.pause(); hero.classList.remove('film-playing'); button.hidden = true; };
video.addEventListener('error', failed);
video.querySelector('source').addEventListener('error', failed);
button.addEventListener('click', () => {
  if (video.paused) { userPaused = false; void play(); }
  else { userPaused = true; ++attempt; video.pause(); }
});
reduced.addEventListener('change', applyPreference);
applyPreference();
})();

