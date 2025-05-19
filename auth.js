// Initialize Firebase
const firebaseConfig = {
  apiKey: "AIzaSyD6Owk17mGhFQvw6Ojv9icz04H8ltaSiEc",
  authDomain: "barberradar-af8ac.firebaseapp.com",
  projectId: "barberradar-af8ac",
  storageBucket: "barberradar-af8ac.firebasestorage.app",
  messagingSenderId: "865081906341",
  appId: "1:865081906341:android:dcb35f42f3deae18f1e94c"
};

// Initialize Firebase
firebase.initializeApp(firebaseConfig);

// Get DOM elements
const signInForm = document.getElementById('signInForm');
const signUpForm = document.getElementById('signUpForm');
const switchToSignUp = document.getElementById('switchToSignUp');
const googleSignInBtn = document.getElementById('googleSignInBtn');
const googleSignUpBtn = document.getElementById('googleSignUpBtn');

// Form switching
switchToSignUp.addEventListener('click', () => {
    signInForm.style.display = 'none';
    signUpForm.style.display = 'block';
    switchToSignUp.textContent = 'Sign In';
    switchToSignUp.style.textDecoration = 'none';
    switchToSignUp.onclick = () => {
        signInForm.style.display = 'block';
        signUpForm.style.display = 'none';
        switchToSignUp.textContent = 'Sign Up';
        switchToSignUp.style.textDecoration = 'underline';
    }
});

// Sign In Form
signInForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const email = document.getElementById('signInEmail').value;
    const password = document.getElementById('signInPassword').value;

    try {
        const userCredential = await firebase.auth().signInWithEmailAndPassword(email, password);
        console.log('User signed in:', userCredential.user);
        // Redirect or show success message
    } catch (error) {
        console.error('Sign in error:', error);
        alert(error.message);
    }
});

// Sign Up Form
signUpForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const fullName = document.getElementById('fullName').value;
    const address = document.getElementById('address').value;
    const email = document.getElementById('signUpEmail').value;
    const password = document.getElementById('signUpPassword').value;
    const confirmPassword = document.getElementById('confirmPassword').value;

    if (password !== confirmPassword) {
        alert('Passwords do not match!');
        return;
    }

    try {
        const userCredential = await firebase.auth().createUserWithEmailAndPassword(email, password);
        
        // Create user profile in Firestore
        await firebase.firestore().collection('users').doc(userCredential.user.uid).set({
            fullName,
            address,
            email,
            createdAt: firebase.firestore.FieldValue.serverTimestamp()
        });

        console.log('User signed up:', userCredential.user);
        // Redirect or show success message
    } catch (error) {
        console.error('Sign up error:', error);
        alert(error.message);
    }
});

// Google Sign In
const googleProvider = new firebase.auth.GoogleAuthProvider();

googleSignInBtn.addEventListener('click', async () => {
    try {
        const result = await firebase.auth().signInWithPopup(googleProvider);
        console.log('Google sign in successful:', result.user);
    } catch (error) {
        console.error('Google sign in error:', error);
        alert(error.message);
    }
});

googleSignUpBtn.addEventListener('click', async () => {
    try {
        const result = await firebase.auth().signInWithPopup(googleProvider);
        console.log('Google sign up successful:', result.user);
        
        // Create user profile in Firestore
        await firebase.firestore().collection('users').doc(result.user.uid).set({
            fullName: result.user.displayName,
            address: '', // Can be updated later
            email: result.user.email,
            createdAt: firebase.firestore.FieldValue.serverTimestamp()
        });
    } catch (error) {
        console.error('Google sign up error:', error);
        alert(error.message);
    }
});
