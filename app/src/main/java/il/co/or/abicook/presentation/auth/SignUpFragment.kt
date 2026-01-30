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

class SignUpFragment : Fragment() {

    private val viewModel: SignUpViewModel by viewModels {
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
    ): View? = inflater.inflate(R.layout.fragment_sign_up, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etUsername = view.findViewById<TextInputEditText>(R.id.etUsername)
        val etEmail = view.findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword = view.findViewById<TextInputEditText>(R.id.etPassword)
        val etConfirmPassword = view.findViewById<TextInputEditText>(R.id.etConfirmPassword)
        val btnSignUp = view.findViewById<MaterialButton>(R.id.btnSignUp)
        val btnBackToLogin = view.findViewById<MaterialButton>(R.id.btnBackToLogin)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            progressBar?.isVisible = state.isLoading
            btnSignUp.isEnabled = !state.isLoading

            if (state.error != null) {
                Toast.makeText(requireContext(), state.error, Toast.LENGTH_SHORT).show()
            }

            if (state.success) {
                Toast.makeText(
                    requireContext(),
                    "Account created successfully! Please log in 😊",
                    Toast.LENGTH_SHORT
                ).show()

                findNavController().navigate(
                    R.id.action_signUpFragment_to_loginFragment
                )
            }
        }

        btnSignUp.setOnClickListener {
            val username = etUsername.text?.toString().orEmpty()
            val email = etEmail.text?.toString().orEmpty()
            val password = etPassword.text?.toString().orEmpty()
            val confirmPassword = etConfirmPassword.text?.toString().orEmpty()

            viewModel.signUp(email, password, confirmPassword, username)
        }

        btnBackToLogin.setOnClickListener {
            findNavController().navigate(R.id.action_signUpFragment_to_loginFragment)
        }
    }
}
