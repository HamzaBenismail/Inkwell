/**
 * Enhanced Spell Check Service for Inkwell
 * This service extends browser's native spell check with additional features
 */

class SpellCheckService {
    constructor() {
        this.dictionary = new Set();
        this.customDictionary = new Set();
        this.initialized = false;
        this.commonWords = null;
    }

    async initialize() {
        if (this.initialized) return;
        
        try {
            // Load common English words dictionary
            const response = await fetch('/js/common-words-dictionary.json');
            if (!response.ok) throw new Error('Failed to load dictionary');
            
            this.commonWords = await response.json();
            this.dictionary = new Set(this.commonWords);
            
            // Load user's custom dictionary from localStorage
            this.loadCustomDictionary();
            
            this.initialized = true;
            console.log('Spell check service initialized');
        } catch (error) {
            console.error('Failed to initialize spell check service:', error);
        }
    }

    loadCustomDictionary() {
        const savedDict = localStorage.getItem('customDictionary');
        if (savedDict) {
            try {
                const words = JSON.parse(savedDict);
                this.customDictionary = new Set(words);
                // Add custom words to main dictionary
                words.forEach(word => this.dictionary.add(word));
            } catch (e) {
                console.error('Error loading custom dictionary:', e);
                this.customDictionary = new Set();
            }
        } else {
            this.customDictionary = new Set();
        }
    }

    saveCustomDictionary() {
        localStorage.setItem('customDictionary', JSON.stringify([...this.customDictionary]));
    }

    addWordToCustomDictionary(word) {
        this.customDictionary.add(word.toLowerCase());
        this.dictionary.add(word.toLowerCase());
        this.saveCustomDictionary();
    }

    checkWord(word) {
        if (!this.initialized) return true;
        
        // Remove punctuation
        const cleanWord = word.replace(/[^\w\s']/g, '').toLowerCase();
        if (!cleanWord || cleanWord.length < 2) return true;
        
        return this.dictionary.has(cleanWord);
    }

    // Check text and return array of misspelled words with positions
    checkText(text) {
        if (!this.initialized) return [];
        
        const words = text.match(/\b[\w']+\b/g) || [];
        const misspelled = [];
        
        for (const word of words) {
            if (!this.checkWord(word)) {
                misspelled.push(word);
            }
        }
        
        return misspelled;
    }

    // Suggest corrections for a misspelled word
    suggestCorrections(word, maxSuggestions = 5) {
        if (!this.initialized || !this.commonWords) return [];
        
        const cleanWord = word.toLowerCase();
        const suggestions = [];
        
        // Simple edit distance algorithm for suggestions
        for (const dictWord of this.commonWords) {
            if (this.levenshteinDistance(cleanWord, dictWord) <= 2) {
                suggestions.push(dictWord);
                if (suggestions.length >= maxSuggestions) break;
            }
        }
        
        return suggestions;
    }

    // Levenshtein distance for word similarity
    levenshteinDistance(word1, word2) {
        const a = word1.toLowerCase();
        const b = word2.toLowerCase();
        
        // Create matrix
        const matrix = Array(a.length + 1).fill().map(() => Array(b.length + 1).fill(0));
        
        // Fill first row and column
        for (let i = 0; i <= a.length; i++) matrix[i][0] = i;
        for (let j = 0; j <= b.length; j++) matrix[0][j] = j;
        
        // Fill the matrix
        for (let i = 1; i <= a.length; i++) {
            for (let j = 1; j <= b.length; j++) {
                const cost = a[i - 1] === b[j - 1] ? 0 : 1;
                matrix[i][j] = Math.min(
                    matrix[i - 1][j] + 1, // deletion
                    matrix[i][j - 1] + 1, // insertion
                    matrix[i - 1][j - 1] + cost // substitution
                );
            }
        }
        
        return matrix[a.length][b.length];
    }
}

// Create global instance
const spellCheckService = new SpellCheckService();

// Initialize on page load
document.addEventListener('DOMContentLoaded', () => {
    // Only initialize if spell check is enabled
    if (localStorage.getItem('spellCheckEnabled') !== 'false') {
        spellCheckService.initialize();
    }
});