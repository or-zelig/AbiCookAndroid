package il.co.or.abicook.presentation.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import il.co.or.abicook.R
import il.co.or.abicook.data.repository.FirebaseAuthRepository

class LoginFragment : Fragment() {

    private val viewModel: LoginViewModel by viewModels {
        AuthViewModelFactory(
            FirebaseAuthRepository(
                FirebaseAuth.getInstance(),
                FirebaseFirestore.getInstance()
            )
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_login, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etEmail = view.findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword = view.findViewById<TextInputEditText>(R.id.etPassword)
        val btnLogin = view.findViewById<MaterialButton>(R.id.btnLogin)
        val btnGoToSignUp = view.findViewById<MaterialButton>(R.id.btnGoToSignUp)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)

        // אם כבר מחובר – דלג ישר ל-Home
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            findNavController().navigate(
                R.id.homeFeedFragment,
                null,
                androidx.navigation.navOptions {
                    popUpTo(R.id.loginFragment) { inclusive = true }
                    launchSingleTop = true
                }
            )

            return
        }

        // האזנה למצב ה-UI
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            progressBar.isVisible = state.isLoading
            btnLogin.isEnabled = !state.isLoading
            btnGoToSignUp.isEnabled = !state.isLoading

            if (state.error != null) {
                Toast.makeText(requireContext(), state.error, Toast.LENGTH_SHORT).show()
            }

            if (state.success) {
                Toast.makeText(
                    requireContext(),
                    "Welcome back! 😄",
                    Toast.LENGTH_SHORT
                ).show()

                findNavController().navigate(
                    R.id.homeFeedFragment,
                    null,
                    androidx.navigation.navOptions {
                        popUpTo(R.id.loginFragment) { inclusive = true }
                        launchSingleTop = true
                    }
                )

            }
        }

        btnLogin.setOnClickListener {
            val email = etEmail.text?.toString().orEmpty()
            val password = etPassword.text?.toString().orEmpty()
            viewModel.login(email, password)
        }

        btnGoToSignUp.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_signUpFragment)
        }
    }
}
