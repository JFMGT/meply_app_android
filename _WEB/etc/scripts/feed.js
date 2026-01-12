function nextSlide(id) {
  const slider = document.getElementById(id);
  const slides = slider.querySelectorAll('.slide');
  const activeIndex = Array.from(slides).findIndex(s => s.classList.contains('active'));
  if (activeIndex !== -1) {
    slides[activeIndex].classList.remove('active');
    const nextIndex = (activeIndex + 1) % slides.length;
    slides[nextIndex].classList.add('active');
  }
}

function prevSlide(id) {
  const slider = document.getElementById(id);
  const slides = slider.querySelectorAll('.slide');
  const activeIndex = Array.from(slides).findIndex(s => s.classList.contains('active'));
  if (activeIndex !== -1) {
    slides[activeIndex].classList.remove('active');
    const prevIndex = (activeIndex - 1 + slides.length) % slides.length;
    slides[prevIndex].classList.add('active');
  }
}

function openLightbox(url, alt) {
  const box = document.getElementById('lightbox');
  const img = box.querySelector('.lightbox-image');
  img.src = url;
  img.alt = alt;
  box.querySelector('.lightbox-caption').textContent = alt;
  box.classList.add('visible');
}

function closeLightbox() {
  const box = document.getElementById('lightbox');
  box.classList.remove('visible');
  box.querySelector('.lightbox-image').src = '';
}

// ESC-Taste zum Schließen
document.addEventListener('keydown', (e) => {
  if (e.key === 'Escape') closeLightbox();
});