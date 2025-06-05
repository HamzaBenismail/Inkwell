// Function to display error messages
function displayError(message) {
    const errorDiv = document.getElementById("error-message")
    if (errorDiv) {
      errorDiv.textContent = message
      errorDiv.classList.remove("hidden")
    }
  }
  
  // Function to get URL parameters
  function getUrlParameter(name) {
    name = name.replace(/[[]/, "\\[").replace(/[\]]/, "\\]")
    var regex = new RegExp("[\\?&]" + name + "=([^&#]*)")
    var results = regex.exec(location.search)
    return results === null ? "" : decodeURIComponent(results[1].replace(/\+/g, " "))
  }
  
  // Check for error parameter in URL
  document.addEventListener("DOMContentLoaded", () => {
    const error = getUrlParameter("error")
    if (error === "true") {
      // Get error message from session if available
      const sessionError = document.getElementById("session-error")
      const errorMessage = sessionError ? sessionError.value : "Invalid username/email or password"
      displayError(errorMessage)
    }
  })
  
  