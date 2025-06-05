class EditorService {
    constructor(editorElement) {
      this.editor = editorElement
      this.setupEditor()
    }
  
    setupEditor() {
      // Make the editor element editable
      this.editor.contentEditable = "true"
      this.editor.className = "w-full h-full bg-gray-700 rounded-lg p-4 focus:outline-none overflow-y-auto"
    }
  
    execCommand(command, value = null) {
      document.execCommand(command, false, value)
      this.editor.focus()
    }
  
    // Formatting methods
    bold() {
      this.execCommand("bold")
    }
  
    italic() {
      this.execCommand("italic")
    }
  
    underline() {
      this.execCommand("underline")
    }
  
    alignLeft() {
      this.execCommand("justifyLeft")
    }
  
    alignCenter() {
      this.execCommand("justifyCenter")
    }
  
    alignRight() {
      this.execCommand("justifyRight")
    }
  
    insertUnorderedList() {
      this.execCommand("insertUnorderedList")
    }
  
    insertOrderedList() {
      this.execCommand("insertOrderedList")
    }
  
    // Get content
    getContent() {
      return this.editor.innerHTML
    }
  
    // Set content
    setContent(content) {
      this.editor.innerHTML = content
    }
  }
  
  