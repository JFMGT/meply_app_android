package de.meply.meply.ui.pm
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.fragment.app.Fragment

class PmFragment : Fragment() {
    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        return TextView(requireContext()).apply { text = "PM" }
    }
}