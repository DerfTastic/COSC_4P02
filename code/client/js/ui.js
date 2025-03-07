function getItemsPerView() {
    if (window.innerWidth <= 480) return 1;
    if (window.innerWidth <= 768) return 2;
    return 4; 
}

document.querySelectorAll('.carousel').forEach((carousel) => {
    let currentIndex = 0;
    const productContainer = carousel.querySelector('.product-container');
    const productBoxes = carousel.querySelectorAll('.product-box');
    const totalSlides = productBoxes.length;
    let animationTimeout;

    const updateCarousel = () => {
        const slideWidth = productBoxes[0].offsetWidth + parseInt(getComputedStyle(productContainer).gap);
        productContainer.style.animation= 'none';
        productContainer.style.transform = `translateX(-${currentIndex * slideWidth}px)`;
        productContainer.style.transition = 'transform 0.3s ease';
        clearTimeout(animationTimeout);
        animationTimeout = setTimeout(() => {
            productContainer.style.animation = 'scroll 20s infinite linear';
        }, 3000);
    };
  

    carousel.querySelector('.arrow.left').addEventListener('click', () => {
        currentIndex = (currentIndex > 0) ? currentIndex - 1 : totalSlides - getItemsPerView();
        updateCarousel();
    });

    carousel.querySelector('.arrow.right').addEventListener('click', () => {
        currentIndex = (currentIndex < totalSlides - getItemsPerView()) ? currentIndex + 1 : 0;  
        updateCarousel();
    });
});
