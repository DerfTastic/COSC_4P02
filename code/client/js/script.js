

document.addEventListener('DOMContentLoaded', () => {
    fetch('/header.html')
    .then(response => {
        if (!response.ok) {
            throw new Error('Network response was not OK');
        }
        return response.text();
    })
    .then(data => {
        document.getElementById('header').innerHTML = data;
        const hamburger = document.querySelector('.hamburger');
            hamburger.addEventListener('click', () => {
                const nav = document.querySelector('nav');
                nav.classList.toggle('active'); 
            });
    })
    .catch(error => {
        console.error('Error loading header:', error);
    });
    

    fetch('/footer.html')
    .then(response => {
        if (!response.ok) {
            throw new Error('Network response was not OK');
        }
        return response.text();
    })
    .then(data => {
        document.getElementById('footer').innerHTML = data;
       
    })
    .catch(error => {
        console.error('Error loading footer:', error);
    });
    

}

);


    function toggleMenu() {
        const hamburger = document.querySelector('.hamburger');
        const navLinks = document.querySelector('nav ul');
        if (hamburger && navLinks) {
            hamburger.classList.toggle('active');
            navLinks.classList.toggle('active');
        }
    }




