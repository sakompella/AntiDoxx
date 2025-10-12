const URL = 'http://localhost:8000';

function URLfy(path) {
    return URL + path;
}

function displayFormattedResponse(message) {
    const responsePanel = document.getElementById('response-panel');

    responsePanel.innerHTML = '';

    const container = document.createElement('div');
    container.className = 'analysis-result';

    const header = document.createElement('div');
    header.className = 'analysis-header';
    header.innerHTML = '<h3>Analysis Results:</h3>';

    const content = document.createElement('div');
    content.className = 'analysis-content';

    const formattedText = message
        .split('\n\n')
        .map(paragraph => {
            if (paragraph.trim().startsWith('**') && paragraph.trim().endsWith('**')) {
                return `<h4>${paragraph.replace(/\*\*/g, '')}</h4>`;
            }

            if (paragraph.includes('•') || paragraph.includes('-')) {
                const items = paragraph.split('\n')
                    .filter(item => item.trim())
                    .map(item => `<li>${item.replace(/^[•\-]\s*/, '')}</li>`)
                    .join('');
                return `<ul>${items}</ul>`;
            }

            let formatted = paragraph
                .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
                .replace(/\*(.*?)\*/g, '<em>$1</em>')
                .replace(/`(.*?)`/g, '<code>$1</code>');

            return `<p>${formatted}</p>`;
        })
        .join('');

    content.innerHTML = formattedText;

    container.appendChild(header);
    container.appendChild(content);
    responsePanel.appendChild(container);
}

document.addEventListener('DOMContentLoaded', () => {
    const fileUploadForm = document.getElementById('fileUploadForm');
    const fileInput = document.getElementById('fileInput');
    const statusMessage = document.getElementById('statusMessage');
    const textUploadForm = document.getElementById('textUploadForm');
    const textInput = document.getElementById('textInput');
    const textStatusMessage = document.getElementById('textStatusMessage');
    const darkModeToggle = document.getElementById('theme-toggle');
    const responsePanel = document.getElementById('response-panel');
    const root = document.documentElement;
    const loader = document.getElementById('load-div')
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
        loader.classList.remove('loader-div');

        let uploadResponse;
        try {
            uploadResponse = await fetch(URLfy('/upload-file'), { // **Replace with your backend URL**
                method: 'POST',
                // When using FormData, the 'Content-Type' header 
                // is automatically set correctly by the browser, 
                // including the boundary required for 'multipart/form-data'. 
                // Do NOT set it manually.
                body: formData
            });
        } catch (error) {
            console.error('Network error:', error);
            statusMessage.textContent = 'A network error occurred during upload.';
            return;
        }

        if (!uploadResponse.ok) {
            const errorText = await uploadResponse.text();
            statusMessage.textContent = `Upload failed. Status: ${uploadResponse.status}. Error: ${errorText.substring(0, 50)}...`;
            console.log(errorText);
            return;
        }
        let result;
        try {
            result = await uploadResponse.json();
        } catch (jsonError) {
            console.error('Error parsing upload response JSON:', jsonError);
            statusMessage.textContent = 'Upload failed: Invalid response format.';
            return;
        }
        statusMessage.textContent = `Upload successful!`;
        fileUploadForm.reset(); // Clear the form

        let adviceResponse;
        try {
            adviceResponse = await fetch(URLfy(`/file-advice?filename=${encodeURIComponent(result.filename)}`), {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json',
                }
            });
        } catch (error) {
            console.error('Network error:', error);
            statusMessage.textContent = 'A network error occurred during upload.';
            return;
        }

        if (!adviceResponse.ok) {
            const errorText = await adviceResponse.text();
            statusMessage.textContent = `Upload failed. Status: ${adviceResponse.status}. Error: ${errorText.substring(0, 50)}...`;
            console.log(errorText);
            return;
        }

        let adviceResult;
        try {
            adviceResult = await adviceResponse.json();
        } catch (jsonError) {
            console.error('Error parsing advice response JSON:', jsonError);
            statusMessage.textContent = 'Error: Invalid response format from advice service.';
            return;
        }
        loader.classList.add('loader-div');
        displayFormattedResponse(adviceResult.message);
    });

    textUploadForm.addEventListener('submit', async (event) => {
        event.preventDefault(); // Prevent the form from reloading the page

        const text = textInput.value.trim();
        if (!text) {
            textStatusMessage.textContent = 'Please enter some text to submit.';
            return;
        }

        textStatusMessage.textContent = 'Submitting...';
        loader.classList.remove('active');
        try {
            // **Replace with your backend URL for text submission**
            const response = await fetch(URLfy(`/text-advice?text=${encodeURIComponent(text)}`), {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json',
                }
            });

            if (response.ok) {
                let result;
                loader.classList.add('loader-div');
                try {
                    result = await response.json();
                } catch (jsonError) {
                    console.error('Error parsing text response JSON:', jsonError);
                    textStatusMessage.textContent = 'Submission failed: Invalid response format.';
                    return;
                }
                textStatusMessage.textContent = 'Submission successful!';
                displayFormattedResponse(result.message);
                textUploadForm.reset();
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