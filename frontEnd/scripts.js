document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('fileUploadForm');
    const fileInput = document.getElementById('fileInput');
    const statusMessage = document.getElementById('statusMessage');
    const toggleButton = document.getElementById('theme-toggle');
    const root = document.documentElement;

    const tabContainer = document.querySelector('.tab-container');
    const tabContents = document.querySelectorAll('.tab-content');

    tabContainer.addEventListener('click', (e) => {
        if (e.target.matches('.tab-button')) {
            // Deactivate all tabs
            tabContainer.querySelectorAll('.tab-button').forEach(button => button.classList.remove('active'));
            tabContents.forEach(content => content.classList.add('hidden'));

            // Activate the clicked tab
            e.target.classList.add('active');
            const targetContentId = e.target.dataset.tab;
            document.getElementById(targetContentId).classList.remove('hidden');
        }
    });

    const savedTheme = localStorage.getItem('theme');
    if(savedTheme) {
        root.style.colorScheme = savedTheme;
    }

    toggleButton.addEventListener('click', () => {
        const currentScheme = root.style.colorScheme === 'dark' ? 'light' : 'dark';
        root.style.colorScheme = currentScheme;
        localStorage.setItem('theme', currentScheme);
    });

    form.addEventListener('submit', async (event) => {
        event.preventDefault(); // Stop the default form submission

        if (fileInput.files.length === 0) {
            statusMessage.textContent = 'Please select a file to upload.';
            return;
        }

        const file = fileInput.files[0];
        const formData = new FormData();
        
        
        // Append the file to the FormData object.
        // The 'uploadedFile' key must match the name your backend expects.
        formData.append('uploadedFile', file); 

        // Optional: Append other data if needed
        // formData.append('userId', '12345'); 

        statusMessage.textContent = 'Uploading...';

        try {
            const response = await fetch('http://localhost:8000/upload-file', { // **Replace with your backend URL**
                method: 'POST',
                // When using FormData, the 'Content-Type' header 
                // is automatically set correctly by the browser, 
                // including the boundary required for 'multipart/form-data'. 
                // Do NOT set it manually.
                body: formData 
            });

            if (response.ok) {
                const result = await response.json(); // Assuming your backend returns JSON
                statusMessage.textContent = `Upload successful! Response: ${result.message}`;
                form.reset(); // Clear the form
            } else {
                const errorText = await response.text();
                statusMessage.textContent = `Upload failed. Status: ${response.status}. Error: ${errorText.substring(0, 50)}...`;
            }
        } catch (error) {
            console.error('Network error:', error);
            statusMessage.textContent = 'A network error occurred during upload.';
        }
    });
});