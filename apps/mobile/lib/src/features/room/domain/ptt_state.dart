enum PttPhase { idle, requesting, transmitting, denied, error }

class FloorLease {
  const FloorLease({
    required this.granted,
    required this.speakerId,
    required this.leaseMillis,
  });

  final bool granted;
  final String? speakerId;
  final int leaseMillis;
}

class PttUiState {
  const PttUiState({
    this.phase = PttPhase.idle,
    this.speakerId,
    this.message = '按住说话',
  });

  final PttPhase phase;
  final String? speakerId;
  final String message;

  PttUiState copyWith({
    PttPhase? phase,
    String? speakerId,
    bool clearSpeaker = false,
    String? message,
  }) {
    return PttUiState(
      phase: phase ?? this.phase,
      speakerId: clearSpeaker ? null : (speakerId ?? this.speakerId),
      message: message ?? this.message,
    );
  }
}
