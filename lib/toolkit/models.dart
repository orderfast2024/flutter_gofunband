class GoBandUser {
  final String id;
  final double balance;

  const GoBandUser({
    required this.id,
    required this.balance,
  });

  factory GoBandUser.fromJson(Map<String, dynamic> json) {
    return GoBandUser(
      id: json['id'] as String,
      balance: (json['balance'] as num).toDouble(),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'balance': balance,
    };
  }

}