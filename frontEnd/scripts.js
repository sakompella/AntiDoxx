const URL = 'http://localhost:8000';

function URLfy(path) {
    return URL + path;
}

document.addEventListener('DOMContentLoaded', () => {
    const fileUploadForm = document.getElementById('fileUploadForm');
    const fileInput = document.getElementById('fileInput');
    const statusMessage = document.getElementById('statusMessage');
    const textUploadForm = document.getElementById('textUploadForm');
    const textInput = document.getElementById('textInput');
    const textStatusMessage = document.getElementById('textStatusMessage');
    const darkModeToggle = document.getElementById('theme-toggle');
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

    darkModeToggle.addEventListener('click', () => {
        const currentScheme = root.style.colorScheme === 'dark' ? 'light' : 'dark';
        root.style.colorScheme = currentScheme;
        localStorage.setItem('theme', currentScheme);
    });

    fileUploadForm.addEventListener('submit', async (event) => {
        event.preventDefault(); // Stop the default form submission

        if (fileInput.files.length === 0) {
            statusMessage.textContent = 'Please select a file to upload.';
            return;
        }

        const file = fileInput.files[0];
        const formData = new FormData();
        
        
        // Append the file to the FormData object.
        // The 'uploadedFile' key must match the name your backend expects.
        formData.append('file', file);

        // Optional: Append other data if needed
        // formData.append('userId', '12345'); 

        statusMessage.textContent = 'Uploading...';

        try {
            uploadResponse = await fetch(URLfy('/upload-file'), { // **Replace with your backend URL**
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
                fileUploadForm.reset(); // Clear the form
            } else {
                const errorText = await response.text();
                statusMessage.textContent = `Upload failed. Status: ${response.status}. Error: ${errorText.substring(0, 50)}...`;
            }
            adviceResponse = await fetch(URLfy('/file-advice'), {
        } catch (error) {
            console.error('Network error:', error);
            statusMessage.textContent = 'A network error occurred during upload.';
        }
    });

    textUploadForm.addEventListener('submit', async (event) => {
        event.preventDefault(); // Prevent the form from reloading the page

        const text = textInput.value.trim();
        if (!text) {
            textStatusMessage.textContent = 'Please enter some text to submit.';
            return;
        }

        textStatusMessage.textContent = 'Submitting...';

        try {
            // **Replace with your backend URL for text submission**
            const response = await fetch(URLfy('/text-advice'), {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ text: text }) 
            });

            if (response.ok) {
                const result = await response.json(); // Assuming JSON response
                textStatusMessage.textContent = `Submission successful! Response: ${result.message}`;
                textUploadForm.reset(); // Clear the form
            } else {
                const errorText = await response.text();
                textStatusMessage.textContent = `Submission failed. Status: ${response.status}. Error: ${errorText}`;
            }
        } catch (error) {
            console.error('Network error:', error);
            textStatusMessage.textContent = 'A network error occurred during submission.';
        }
    });
});