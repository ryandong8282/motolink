import 'package:flutter_test/flutter_test.dart';
import 'package:motolink_mobile/main.dart';

void main() {
  testWidgets('shows the MotoLink development login', (tester) async {
    await tester.pumpWidget(const MotoLinkApp());

    expect(find.text('MotoLink'), findsOneWidget);
    expect(find.text('进入 MVP'), findsOneWidget);
    expect(find.text('手机号（开发态）'), findsOneWidget);
  });
}
