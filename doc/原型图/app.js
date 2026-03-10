document.querySelectorAll('.card').forEach((card, index) => {
  card.style.animationDelay = `${index * 0.05}s`;
});
