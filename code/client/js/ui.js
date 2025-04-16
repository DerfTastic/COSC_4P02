document.addEventListener("dynamic_content_finished", () => {
    document.querySelectorAll('.carousel').forEach((carousel) => {
        const productContainer = carousel.querySelector('.product-container');
        const productBoxes = carousel.querySelectorAll('.product-box');
        const totalSlides = productBoxes.length;

        let currentIndex = 0;

        const getSlideWidth = () => {
            const box = productBoxes[0];
            const gap = parseInt(getComputedStyle(productContainer).gap || 0);
            return box.getBoundingClientRect().width + gap;
        };

        const scrollToIndex = (index) => {
            const slideWidth = getSlideWidth();
            productContainer.scrollTo({
                left: index * slideWidth,
                behavior: 'smooth'
            });
        };

        // Arrow click handlers
        carousel.querySelector('.arrow.left').addEventListener('click', () => {
            if (currentIndex > 0) {
                currentIndex--;
                scrollToIndex(currentIndex);
            }
        });

        carousel.querySelector('.arrow.right').addEventListener('click', () => {
            const slideWidth = getSlideWidth();
            const visibleItems = Math.floor(productContainer.offsetWidth / slideWidth);
            if (currentIndex < totalSlides - visibleItems) {
                currentIndex++;
                scrollToIndex(currentIndex);
            }
        });

        // Sync currentIndex when user scrolls manually (e.g., touchpad or drag)
        productContainer.addEventListener('scroll', () => {
            const slideWidth = getSlideWidth();
            const newIndex = Math.round(productContainer.scrollLeft / slideWidth);
            if (newIndex !== currentIndex) {
                currentIndex = newIndex;
            }
        });

        // Resize event: just re-align to current index
        window.addEventListener('resize', () => {
            scrollToIndex(currentIndex);
        });
    });
});
