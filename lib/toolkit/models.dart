
/// Datos del usuario leído del tag
class TagUser {
  final String userId;
  final int userBalance;
  final String reference;
  final String tagId;
  final int category;
  final bool isPaid;

  TagUser({
    required this.userId,
    required this.userBalance,
    required this.reference,
    required this.tagId,
    required this.category,
    required this.isPaid,
  });

  factory TagUser.fromMap(Map<dynamic, dynamic> map) {
    return TagUser(
      userId: map['userId'] as String,
      userBalance: map['userBalance'] as int,
      reference: map['reference'] as String? ?? '',
      tagId: map['tagId'] as String,
      category: map['category'] as int? ?? 0,
      isPaid: map['isPaid'] as bool? ?? false,
    );
  }

  Map<String, dynamic> toMap() {
    return {
      'userId': userId,
      'userBalance': userBalance,
      'reference': reference,
      'tagId': tagId,
      'category': category,
      'isPaid': isPaid,
    };
  }

  @override
  String toString() {
    return 'TagUser(userId: $userId, balance: $userBalance, reference: $reference)';
  }
}

/// Resultado de una recarga
class RechargeResult {
  final int amount;
  final String concept;
  final String? reference;

  RechargeResult({
    required this.amount,
    required this.concept,
    this.reference,
  });

  factory RechargeResult.fromMap(Map<dynamic, dynamic> map) {
    return RechargeResult(
      amount: map['amount'] as int,
      concept: map['concept'] as String,
      reference: map['reference'] as String?,
    );
  }
}